export type Partner = {
  id: number;
  name: string;
  billingDate: string;
  status: "Activé";
  pack: "Gold" | "Silver" | "Bronze";
};

export type Event = {
  id: string;
  name: string;
  start_time: Date;
  end_time: Date;
  submission_start_time: Date;
  submission_end_time: Date;
};

export type Owner = {
  display_name: string;
  email: string;
};

export type Organization = {
  name: string;
  slug: string;
  head_office: string;
  owner: Owner;
};
