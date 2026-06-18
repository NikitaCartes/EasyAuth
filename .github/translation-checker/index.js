/*
That script was originally written by (Chicken) Antti Ellilä and TBlueF (Lukas Rieger) as part of the BlueMap project.
Licensed under same license as the rest of the project (MIT).
*/

import { execSync } from "node:child_process";
import { readdirSync } from "node:fs";
import path from "node:path";

// doesn't really matter, just setting something consistant and close enough for us europeans
process.env.TZ = "Europe/Berlin";

function parse(str) {
    const blame = execSync(`git blame --porcelain ${str}`).toString("utf8").trim().split("\n");
    const commitMap = new Map();
    const nodes = [];
    const stack = []; // { indent, key }
    for (let i = 0; i < blame.length; i++) {
        const hash = blame[i].split(" ")[0];
        i++;
        if (!commitMap.has(hash)) {
            const commit = {};
            let j = 0;
            while (true) {
                const line = blame[i + j];
                if (line[0] === "\t") break;
                const [key, ...rest] = line.split(" ");
                const val = rest.join(" ");
                commit[key.replace(/-\S/g, (s) => s.slice(1).toUpperCase())] = val;
                j++;
            }
            commitMap.set(hash, commit);
            i += j;
        }
        const commit = commitMap.get(hash);
        const lastUpdated = parseInt(commit.authorTime);

        const raw = blame[i].slice(1); // source line, leading tab stripped, indentation kept
        if (!raw.trim() || raw.trim().startsWith("#")) continue; // blank / comment
        const indent = raw.length - raw.trimStart().length;
        const line = raw.trim();
        while (stack.length && stack[stack.length - 1].indent >= indent) stack.pop();
        const idx = line.indexOf(":");
        const key = line.slice(0, idx);
        const rest = line.slice(idx + 1).trim();
        if (rest === "") {
            stack.push({ indent, key }); // mapping node
        } else {
            nodes.push({ path: [...stack.map((s) => s.key), key], lastUpdated }); // leaf
        }
    }
    return nodes;
}

const langFolder = "../../src/main/resources/data/easyauth/lang"
const languageFiles = readdirSync(langFolder).filter(
    (f) => f.endsWith(".yml")
);

const languages = languageFiles.map((file) => {
    const nodes = parse(path.join(langFolder, file));
    const name = file.split(".").reverse().slice(1).reverse().join(".");
    return {
        name,
        nodes,
    };
});

const sourceLanguageName = "en_us";
const sourceLanguage = languages.find((l) => l.name === sourceLanguageName);
if (!sourceLanguage) throw new Error(`Source language "${sourceLanguageName}" not found!`);
languages.splice(languages.indexOf(sourceLanguage), 1);

function diff(source, other) {
    const sourceKeys = source.map((n) => n.path.join("."));
    const otherKeys = other.map((n) => n.path.join("."));
    const missing = sourceKeys.filter((sk) => !otherKeys.includes(sk));
    const extra = otherKeys.filter((ok) => !sourceKeys.includes(ok));
    const outdated = other
        .map((n) => {
            const sourceNode = source.find((sn) => sn.path.join(".") === n.path.join("."));
            return { ...n, sourceNode };
        })
        .filter((n) => {
            return n.sourceNode && n.sourceNode.lastUpdated > n.lastUpdated;
        });
    return {
        missing,
        extra,
        outdated,
    };
}

const upToDate = [];
for (const { name, nodes } of languages) {
    const { missing, extra, outdated } = diff(sourceLanguage.nodes, nodes);

    if (missing.length + extra.length + outdated.length === 0) {
        upToDate.push(name);
        continue;
    }

    console.log(`=== ${name} ===`);
    if (missing.length) {
        console.log(`Missing (${missing.length}):`);
        for (const key of missing) console.log("-", key);
        console.log();
    }
    if (extra.length) {
        console.log(`Extra (${extra.length}):`);
        for (const key of extra) console.log("-", key);
        console.log();
    }
    if (outdated.length) {
        console.log(`Outdated (${outdated.length}):`);
        for (const { path, lastUpdated, sourceNode } of outdated)
            console.log(
                "-",
                path.join("."),
                `(updated ${new Date(lastUpdated * 1000).toLocaleString(
                    "en-GB"
                )}, source updated ${new Date(sourceNode.lastUpdated * 1000).toLocaleString("en-GB")})`
            );
        console.log();
    }
}

if (upToDate.length) console.log("Up to date:", upToDate.join(", "));
